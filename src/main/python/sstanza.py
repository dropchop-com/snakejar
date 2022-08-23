import os
import stanza
import threading

from stanza import Pipeline as SPipeline
from typing import List, Dict, Any


class SStanza:
    models: Dict[str, SPipeline] = {}

    def __init__(self):
        pass


__sstanza = SStanza()


def load(model_name: str, config: Dict[str, Any]):
    __sstanza.models[model_name] = stanza.Pipeline(**config)


def reload_model(model_name: str, str_path: str):
    parts = model_name.split("-")
    lang = parts[0]
    config = {
        'dir': os.path.join(str_path, 'resources'),
        'lang': lang,
        'logging_level': 'INFO',
        'tokenize_pretokenized': False,
        'use_gpu': True,
        'download_method': 2
    }
    # thread = threading.Thread(target=load, args=(model_name, config))
    # thread.start()
    # thread.join()
    __sstanza.models[model_name] = stanza.Pipeline(**config)


def sstanza(model_name: str, text: str, processors: List[str] = None):
    doc = __sstanza.models[model_name].process(text, processors)
    return doc


def cclassla_tokenized(model_name: str, text: List, processors: List[str] = None):
    doc = __sstanza.models[model_name].process(text, processors)
    return doc


if __name__ == "__main__":
    # stanza.download('sl', "../../models/stanza/resources")
    reload_model("sl-ssj", "models/stanza")
    text = "Popoldne bo deloma sončno, nastale bodo krajevne plohe in nevihte. V nedeljo bo pretežno jasno"
    ret = sstanza("sl-ssj", text)
    print(ret)
