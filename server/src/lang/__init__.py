import importlib
from typing import Any, Dict

class Lang():
    def __init__(self, lang: str):
        self.lang = lang

    def t(self, key: str, **kwargs: Dict[str, Any]) -> str:
        locale_module = importlib.import_module(f'lang.{self.lang}')

        translation = locale_module.locale
        translation = translation.get(key, None)
        if translation is None:
            return f'Key {key} not found in {self.lang} locale'
        if kwargs.keys():
            translation = translation.format(**kwargs)
        return translation
