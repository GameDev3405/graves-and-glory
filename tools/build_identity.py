import json
import pathlib
import sys
from release_config import source_identity

target = pathlib.Path(sys.argv[1])
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps(source_identity(), indent=2) + '\n', encoding='utf8')
print('Recorded release source identity')
