import argparse
import secrets
import sys
from db import connect_to_db, account
import asyncio
import base64
import msgpack

from db.column_cryptor import ColumnCryptor
from models.account import AccountType

async def main():
    parser = argparse.ArgumentParser(description='Register new users to mhealthy.')

# Add arguments
    parser.add_argument('name', type=str, help='The name of the user')
    parser.add_argument('cin', type=str, help='The CIN of the user')
    parser.add_argument('type', type=str, help='The account type', choices=["patient", "caregiver", "selfcarepatient"])
    parser.add_argument('description', type=str, help='A brief description of the patient / caregiver')
    parser.add_argument('phone', type=str, help='Phone number contact')
    parser.add_argument('--caregiver', type=int, help='Caregiver id assigned to patient')

    args = parser.parse_args()

        
    cse = ColumnCryptor()
    db  = await connect_to_db()
    
    # checking if a user with same cin already exists
    exists = await account.cin_exists(db, cse.gen_hash(args.cin.encode()))
    if exists is True:
        print(f"A user with cin={args.cin} already exists", file=sys.stderr)
        sys.exit(1)


    caregiver = None
    if args.type == "patient":
        if args.caregiver is None:
            print(f"Option \"--caregiver\" must be used to assign default caregiver to patient", file=sys.stderr)
            sys.exit(1)
        atype = await account.account_type(db, args.caregiver)
        if atype is None:
            print(f"Caregiver with id={args.caregiver} does not exist", file=sys.stderr)
            sys.exit(1)
        elif atype != AccountType.CareGiver:
            print(f"User with id={args.caregiver} is not a caregiver", file=sys.stderr)
            sys.exit(1)
        caregiver = args.caregiver

    token = secrets.token_bytes(64)
    try:
        newid = await account.create(
            db, cse,
            token,
            args.name, args.cin, AccountType(args.type),
            args.description, args.phone,
            caregiver=caregiver
        )
    except Exception as e:
        print(f"Failed to create user: {e}", file=sys.stderr)
        sys.exit(1)

    nonce = cse.gen_nonce()
    uid   = cse.encrypt(newid.to_bytes(4, byteorder='big'), nonce)
    tpack = msgpack.packb({
        "t": token,
        "id": uid,
        "n": nonce
    })
    tb64 = base64.urlsafe_b64encode(tpack).decode('utf-8')

    print(f"token: {tb64}")
    print(f"QR path: /v1/join_qr/{tb64}")

if __name__ == '__main__':
    asyncio.run(main())
