import argparse
import secrets

parser = argparse.ArgumentParser(description='Register new users to mhealthy.')

# Add arguments
parser.add_argument('name', type=str, help='The name of the user')
parser.add_argument('cin', type=str, help='The CIN of the user')
parser.add_argument('type', type=str, help='The account type', choices=["patient", "caregiver", "selfcarepatient"])
parser.add_argument('description', type=str, help='A brief description of the patient / caregiver')

args = parser.parse_args()

# TODO: Insert to database making sure cin is unique


token = secrets.token_urlsafe(96)

# TODO: Store token
print(f"token: {token}")
print(f"QR path: /v1/join_qr/{token}")
