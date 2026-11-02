"""Named Binary Tag reader and writer (Java edition, big endian), stdlib only.

Tag ids, the gzip wrapper and the DataVersion follow tools/preflight.py (structure_header / nbt_skip), which only
skips tags; this module reads and writes every value so files round-trip byte for byte.
"""
import gzip
import io
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)
NAMES = ('end', 'byte', 'short', 'int', 'long', 'float', 'double', 'byte_array', 'string', 'list', 'compound', 'int_array', 'long_array')
_SCALAR = {BYTE: '>b', SHORT: '>h', INT: '>i', LONG: '>q', FLOAT: '>f', DOUBLE: '>d'}


class Tag:
    """One tag: kind (tag id), value, and for a list the element kind.
    compound value: dict name -> Tag (insertion order kept); list value: list of Tag; byte_array: bytes;
    int_array / long_array: list of int; string: str; numbers: int or float."""
    __slots__ = ('kind', 'value', 'elem')

    def __init__(self, kind, value, elem=END):
        self.kind, self.value, self.elem = kind, value, elem

    def __eq__(self, other):
        return isinstance(other, Tag) and (self.kind, self.value, self.elem) == (other.kind, other.value, other.elem)

    def __repr__(self):
        return f'Tag({NAMES[self.kind]}, {self.value!r})'

    def __getitem__(self, key):
        return self.value[key]

    def get(self, key, default=None):
        return self.value.get(key, default) if self.kind == COMPOUND else default


def byte(v): return Tag(BYTE, int(v))
def short(v): return Tag(SHORT, int(v))
def int_(v): return Tag(INT, int(v))
def long(v): return Tag(LONG, int(v))
def float_(v): return Tag(FLOAT, float(v))
def double(v): return Tag(DOUBLE, float(v))
def string(v): return Tag(STRING, str(v))
def compound(d=None): return Tag(COMPOUND, dict(d or {}))
def int_array(v): return Tag(INT_ARRAY, [int(x) for x in v])


def list_(elem, items):
    items = list(items)
    if any(t.kind != elem for t in items):
        raise ValueError(f'list of {NAMES[elem]} holds another kind')
    return Tag(LIST, items, elem)


def _mutf8_decode(raw):
    """Java modified UTF-8: NUL is C0 80 and supplementary characters are surrogate pairs."""
    text = raw.replace(b'\xc0\x80', b'\x00').decode('utf-8', 'surrogatepass')
    return text.encode('utf-16', 'surrogatepass').decode('utf-16')


def _mutf8_encode(text):
    out = bytearray()
    for ch in text:
        cp = ord(ch)
        if cp == 0:
            out += b'\xc0\x80'
        elif cp > 0xFFFF:
            for unit in struct.unpack('>2H', ch.encode('utf-16-be')):
                out += chr(unit).encode('utf-8', 'surrogatepass')
        else:
            out += ch.encode('utf-8', 'surrogatepass')
    return bytes(out)


class _Reader:
    def __init__(self, buf):
        self.buf, self.pos = buf, 0

    def take(self, fmt):
        v = struct.unpack_from(fmt, self.buf, self.pos)
        self.pos += struct.calcsize(fmt)
        return v[0] if len(v) == 1 else v

    def name(self):
        n = self.take('>H')
        raw = self.buf[self.pos:self.pos + n]
        self.pos += n
        return _mutf8_decode(raw)

    def payload(self, kind, depth=0):
        if depth > 512:
            raise ValueError('NBT nested too deeply')
        if kind in _SCALAR:
            return Tag(kind, self.take(_SCALAR[kind]))
        if kind == BYTE_ARRAY:
            n = self.take('>i')
            v = bytes(self.buf[self.pos:self.pos + n])
            self.pos += n
            return Tag(kind, v)
        if kind == STRING:
            return Tag(kind, self.name())
        if kind == LIST:
            elem, n = self.take('>b'), self.take('>i')
            return Tag(kind, [self.payload(elem, depth + 1) for _ in range(max(n, 0))], elem)
        if kind == COMPOUND:
            d = {}
            while True:
                child = self.take('>b')
                if child == END:
                    return Tag(kind, d)
                key = self.name()
                d[key] = self.payload(child, depth + 1)
        if kind in (INT_ARRAY, LONG_ARRAY):
            n = self.take('>i')
            fmt = '>%d%s' % (n, 'i' if kind == INT_ARRAY else 'q')
            v = list(struct.unpack_from(fmt, self.buf, self.pos))
            self.pos += struct.calcsize(fmt)
            return Tag(kind, v)
        raise ValueError(f'unknown NBT tag {kind}')


def _write_payload(out, tag):
    k = tag.kind
    if k in _SCALAR:
        out.write(struct.pack(_SCALAR[k], tag.value))
    elif k == BYTE_ARRAY:
        out.write(struct.pack('>i', len(tag.value)))
        out.write(tag.value)
    elif k == STRING:
        raw = _mutf8_encode(tag.value)
        out.write(struct.pack('>H', len(raw)))
        out.write(raw)
    elif k == LIST:
        out.write(struct.pack('>bi', tag.elem, len(tag.value)))
        for item in tag.value:
            if item.kind != tag.elem:
                raise ValueError(f'list of {NAMES[tag.elem]} holds a {NAMES[item.kind]}')
            _write_payload(out, item)
    elif k == COMPOUND:
        for key, child in tag.value.items():
            _write_named(out, key, child)
        out.write(b'\x00')
    elif k in (INT_ARRAY, LONG_ARRAY):
        out.write(struct.pack('>i', len(tag.value)))
        out.write(struct.pack('>%d%s' % (len(tag.value), 'i' if k == INT_ARRAY else 'q'), *tag.value))
    else:
        raise ValueError(f'unknown NBT tag {k}')


def _write_named(out, name, tag):
    out.write(struct.pack('>b', tag.kind))
    raw = _mutf8_encode(name)
    out.write(struct.pack('>H', len(raw)))
    out.write(raw)
    _write_payload(out, tag)


def decode(buf):
    """(root name, root Tag) of uncompressed NBT bytes. The root must be a compound."""
    r = _Reader(buf)
    if r.take('>b') != COMPOUND:
        raise ValueError('the root tag is not a compound')
    name = r.name()
    root = r.payload(COMPOUND)
    if r.pos != len(buf):
        raise ValueError(f'{len(buf) - r.pos} trailing bytes after the root compound')
    return name, root


def encode(root, name=''):
    out = io.BytesIO()
    _write_named(out, name, root)
    return out.getvalue()


def read_file(path):
    """(root name, root Tag) of a gzip (or raw) NBT file."""
    with open(path, 'rb') as f:
        raw = f.read()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    return decode(raw)


def write_file(path, root, name=''):
    """Writes gzip NBT with mtime 0, so the same tag always gives the same bytes."""
    data = gzip.compress(encode(root, name), mtime=0)
    with open(path, 'wb') as f:
        f.write(data)


def to_plain(tag):
    """JSON-friendly view for printing (types are lost)."""
    if tag.kind == COMPOUND:
        return {k: to_plain(v) for k, v in tag.value.items()}
    if tag.kind == LIST:
        return [to_plain(v) for v in tag.value]
    if tag.kind == BYTE_ARRAY:
        return list(tag.value)
    return tag.value
