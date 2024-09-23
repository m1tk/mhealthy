import argparse
import sys
from db import connect_to_db
import asyncio

from db.caregiver import assign_caregiver_to_patient, unassign_caregiver
from db.column_cryptor import ColumnCryptor

async def main():
    parser = argparse.ArgumentParser(description='Unassign patient from caregiver')

    parser.add_argument('patient', type=int, help='Patient id')
    parser.add_argument('caregiver', type=int, help='Caregiver id')

    args = parser.parse_args()

        
    cse = ColumnCryptor()
    db  = await connect_to_db()
    
    try:
        await assign_caregiver_to_patient(db, cse, args.caregiver, args.patient, args.caregiver)
    except Exception as e:
        print(f"Failed to create user: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == '__main__':
    asyncio.run(main())
