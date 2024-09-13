import secrets
import argparse
import base64

parser = argparse.ArgumentParser(description='Generate safe secrets to be used for encryption and authentication')
parser.add_argument('output_file', type=str, help='Path to save the secret')

args = parser.parse_args()

secret = secrets.token_bytes(128)
secret = base64.urlsafe_b64encode(secret).decode('utf-8')

with open(args.output_file, 'w') as output_file:
    output_file.write(secret)
