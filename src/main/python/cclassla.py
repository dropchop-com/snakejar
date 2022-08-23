import os
import classla
import threading

from classla import Document as CDocument
from classla.pipeline.registry import PIPELINE_NAMES

from types import MethodType
from typing import List, Dict, Any


def process(self, doc, processors=None):
    """
    Run the pipeline

    processors: allow for a list of processors used by this pipeline action
      can be list, tuple, set, or comma separated string
      if None, use all the processors this pipeline knows about
      MWT is added if necessary
      otherwise, no care is taken to make sure prerequisites are followed...
        some of the annotators, such as depparse, will check, but others
        will fail in some unusual manner or just have really bad results
    """
    assert any([isinstance(doc, str), isinstance(doc, list),
                isinstance(doc, CDocument)]), 'input should be either str, list or Document'

    # determine whether we are in bulk processing mode for multiple documents
    bulk=(isinstance(doc, list) and len(doc) > 0 and isinstance(doc[0], CDocument))

    # various options to limit the processors used by this pipeline action
    if processors is None:
        processors = PIPELINE_NAMES
    elif not isinstance(processors, (str, list, tuple, set)):
        raise ValueError("Cannot process {} as a list of processors to run".format(type(processors)))
    else:
        if isinstance(processors, str):
            processors = {x for x in processors.split(",")}
        else:
            processors = set(processors)
        if 'tokenize' in processors and 'mwt' in self.processors and 'mwt' not in processors:
            #logger.debug("Requested processors for pipeline did not have mwt, but pipeline needs mwt, so mwt is added")
            processors.add('mwt')
        processors = [x for x in PIPELINE_NAMES if x in processors]

    for processor_name in processors:
        if self.processors.get(processor_name):
            process = self.processors[processor_name].bulk_process if bulk else self.processors[processor_name].process
            doc = process(doc)
    return doc


class CClassla:
    models: Dict[str, classla.Pipeline] = {}

    def __init__(self):
        pass


__cclassla = CClassla()


def reload_model_old(model_name: str, str_path: str):
    parts = model_name.split("-")
    lang = parts[0]
    config = {
        'dir': os.path.join(str_path, 'resources'),
        'lang': lang,
        'logging_level': 'DEBUG',
        'tokenize_pretokenized': False,
        'use_gpu': True
    }
    # print("Loading ... ", model_name)
    pipeline = classla.Pipeline(**config)
    classla.Pipeline.process_ext = MethodType(process, pipeline)  # extend with better method
    # print("Loaded ", model_name)
    __cclassla.models[model_name] = pipeline


def load(model_name: str, config: Dict[str, Any]):
    pipeline = classla.Pipeline(**config)
    classla.Pipeline.process_ext = MethodType(process, pipeline)  # extend with better method
    __cclassla.models[model_name] = pipeline


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
    pipeline = classla.Pipeline(**config)
    classla.Pipeline.process_ext = MethodType(process, pipeline)  # extend with better method
    __cclassla.models[model_name] = pipeline


def cclassla(model_name: str, text: str, processors: List[str] = None):
    doc = __cclassla.models[model_name].process_ext(text, processors)
    return doc


def cclassla_tokenized(model_name: str, text: List, processors: List[str] = None):
    doc = __cclassla.models[model_name].process_ext(text, processors)
    return doc


if __name__ == "__main__":
    # classla.download('sl', "../../models/classla/resources")
    reload_model("sl-standard", "../../models/classla")
    text = "Popoldne bo deloma sončno, nastale bodo krajevne plohe in nevihte. V nedeljo bo pretežno jasno"
    ret = cclassla("sl-standard", text, ['tokenize', 'lemma'])
    print(ret)
