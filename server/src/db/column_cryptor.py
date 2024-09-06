import os
import base64
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.scrypt import Scrypt
from cryptography.hazmat.primitives.ciphers.aead import ChaCha20Poly1305

class ColumnCryptor:
    def __init__(self):
        path = os.getenv("ENCRYPTION_KEY")
        if path is None:
            raise Exception("ENCRYPTION_KEY env var is not present")

        with open(path, 'r') as fh:
            key = fh.read()
        key = base64.urlsafe_b64decode(key)
        if len(key) < 100:
            raise Exception("ENCRYPTION_KEY must be bigger than 100 bytes")

        kdf = Scrypt(
            salt=key[:16],
            length=32,
            n=16384,  # CPU/memory cost factor
            r=8,  # Block size
            p=1,  # Parallelization factor
            backend=default_backend()
        )
        self.key = kdf.derive(key)

    def encrypt(self, column: bytes, nonce: bytes) -> bytes:
        chacha = ChaCha20Poly1305(self.key)
        return chacha.encrypt(nonce, column, b"CSE")

    def decrypt(self, column: bytes, nonce: bytes) -> bytes:
        chacha = ChaCha20Poly1305(self.key)
        return chacha.decrypt(nonce, column, b"CSE")

    def gen_nonce(self) -> bytes:
        return os.urandom(12)

    def gen_hash(self, data: bytes) -> bytes:
        digest = hashes.Hash(hashes.SHA256(), backend=default_backend())
        digest.update(data)
        digest.update(self.key[95:])
        return digest.finalize()
