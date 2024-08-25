from enum import Enum
from dataclasses import dataclass

class AccountType(Enum):
    Patient = "patient"
    CareGiver = "caregiver"
    SelfCarerPatient = "selfcarepatient"

@dataclass
class Account:
    name: str
    cin: str
    account_type: AccountType
    description: str
