import os

from ufal.udpipe import Model, Pipeline, ProcessingError # pylint: disable=no-name-in-module


class UDPipe:
    model = {}

    def __init__(self):
        pass


__udpipe = UDPipe()


def reload_model(model_name: str, str_path: str):
    # print("Loading model from [%s]\n" % __file__, file=sys.stderr)
    path = os.path.normpath(str_path)
    __udpipe.model[model_name] = Model.load(path)


def udpipe(model_name: str, text: str):
    pipeline = Pipeline(__udpipe.model[model_name], "tokenize", Pipeline.DEFAULT, Pipeline.DEFAULT, "conllu")
    error = ProcessingError()
    processed = pipeline.process(text, error)
    if error.occurred():
        raise RuntimeError(error.message)

    return processed


if __name__ == "__main__":
    reload_model("slovenian-ssj-ud-2.4-190531", "../../models/udpipe/slovenian-ssj-ud-2.4-190531.udpipe")
    ret = udpipe("slovenian-ssj-ud-2.4-190531", "Popoldne bo deloma sončno, nastale bodo krajevne plohe in nevihte. V nedeljo bo pretežno jasno")
    print(ret)
