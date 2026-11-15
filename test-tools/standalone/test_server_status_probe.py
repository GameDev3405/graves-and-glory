import contextlib
import io
import json
import os
import socket
import sys
import tempfile
import threading
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import server_status_probe as probe  # noqa: E402


def free_port():
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def status_packet(max_players=None, protocol=767):
    body = json.dumps({"version": {"name": "1.21.1", "protocol": protocol},
                       "players": {"max": max_players or probe.expected_max_players(), "online": 0}}).encode()
    payload = probe.varint(0) + probe.varint(len(body)) + body
    return probe.varint(len(payload)) + payload


class FakeServer:
    """Accepts one connection, reads the handshake, writes `reply`, then closes."""

    def __init__(self, reply):
        self.sock = socket.socket()
        self.sock.bind(("127.0.0.1", 0))
        self.sock.listen(1)
        self.port = self.sock.getsockname()[1]
        self.thread = threading.Thread(target=self.serve, args=(reply,), daemon=True)
        self.thread.start()

    def serve(self, reply):
        conn, _ = self.sock.accept()
        with conn:
            conn.settimeout(5)
            conn.recv(4096)
            conn.sendall(reply)

    def close(self):
        self.sock.close()


def run_main(args):
    out = io.StringIO()
    with contextlib.redirect_stdout(out):
        code = probe.main(["server_status_probe.py"] + args)
    return code, json.loads(out.getvalue())


class StatusProbeTest(unittest.TestCase):
    def serve(self, reply):
        server = FakeServer(reply)
        self.addCleanup(server.close)
        return server.port

    def test_port_with_nothing_listening_reports_down(self):
        code, out = run_main([str(free_port())])
        self.assertEqual(code, 1)
        self.assertEqual(out["result"], "DOWN")

    def test_down_server_includes_latest_log_crash(self):
        with tempfile.TemporaryDirectory() as d:
            os.makedirs(os.path.join(d, "logs"))
            os.makedirs(os.path.join(d, "crash-reports"))
            with open(os.path.join(d, "logs", "latest.log"), "w") as f:
                f.write("[main/INFO]: Starting\n[main/ERROR]: Failed to start the minecraft server\n"
                        "java.lang.IllegalStateException: boom\n")
            open(os.path.join(d, "crash-reports", "crash-2026-11-04.txt"), "w").close()
            code, out = run_main([str(free_port()), "--dir", d])
        self.assertEqual((code, out["result"]), (1, "DOWN"))
        self.assertIn("IllegalStateException", out["logs"]["latestLog"])
        self.assertEqual(out["logs"]["crashReport"], "crash-2026-11-04.txt")

    def test_missing_log_folder_does_not_crash(self):
        with tempfile.TemporaryDirectory() as d:
            code, out = run_main([str(free_port()), "--dir", os.path.join(d, "nope")])
        self.assertEqual(code, 1)
        self.assertEqual(out["logs"]["latestLog"], "no logs/latest.log")

    def test_bad_arguments_print_usage(self):
        for args in ([], ["notaport"]):
            code, out = run_main(args)
            self.assertEqual((code, out["result"]), (2, "FAIL"))

    def test_good_status_passes(self):
        code, out = run_main([str(self.serve(status_packet()))])
        self.assertEqual((code, out["result"]), (0, "PASS"))
        self.assertEqual(out["players"]["online"], 0)

    def test_wrong_player_cap_fails_without_traceback(self):
        code, out = run_main([str(self.serve(status_packet(max_players=probe.expected_max_players() + 1)))])
        self.assertEqual((code, out["result"]), (1, "FAIL"))

    def test_server_closing_mid_response_fails_cleanly(self):
        code, out = run_main([str(self.serve(status_packet()[:5]))])
        self.assertEqual((code, out["result"]), (1, "FAIL"))

    def test_server_closing_without_reply_fails_cleanly(self):
        code, out = run_main([str(self.serve(b""))])
        self.assertEqual((code, out["result"]), (1, "FAIL"))

    def test_garbage_json_fails_cleanly(self):
        body = b"not json at all"
        payload = probe.varint(0) + probe.varint(len(body)) + body
        code, out = run_main([str(self.serve(probe.varint(len(payload)) + payload))])
        self.assertEqual((code, out["result"]), (1, "FAIL"))

    def test_silent_server_times_out_cleanly(self):
        server = socket.socket()
        server.bind(("127.0.0.1", 0))
        server.listen(1)
        self.addCleanup(server.close)
        code, out = probe.probe(server.getsockname()[1], timeout=0.5)
        self.assertEqual((code, out["result"]), (1, "FAIL"))


if __name__ == "__main__":
    unittest.main()
