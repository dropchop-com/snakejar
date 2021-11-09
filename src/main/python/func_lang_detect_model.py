import os
import sys
import fasttext

fasttext_model = None


def get_model():
    global fasttext_model
    if not fasttext_model:
        # print("Loading model from [%s]\n" % __file__, file=sys.stderr)
        path = os.path.normpath(os.path.join(os.path.dirname(__file__), 'lid.176.ftz.wiki.fasttext'))
        fasttext_model = fasttext.load_model(path)
    return fasttext_model
