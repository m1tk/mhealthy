from enum import Enum
from dataclasses import dataclass
from typing import Optional

class AccountType(Enum):
    Patient = "patient"
    CareGiver = "caregiver"
    SelfCarerPatient = "selfcarepatient"

def account_type_from_int(account_type: int) -> Optional[AccountType]:
    if account_type == 0:
        return AccountType.Patient
    elif account_type == 1:
        return AccountType.CareGiver
    elif account_type == 2:
        return AccountType.SelfCarerPatient
    else:
        return None

def account_type_to_int(account_type: AccountType) -> int:
    if account_type == AccountType.Patient:
        return 0
    elif account_type == AccountType.CareGiver:
        return 1
    elif account_type == AccountType.SelfCarerPatient:
        return 2

@dataclass
class AccountSession:
    uid: int
    account_type: AccountType
