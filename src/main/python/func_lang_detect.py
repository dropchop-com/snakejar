import func_lang_detect_model

__classification_language_map = {
    '__label__en': 'en',
    '__label__ru': 'ru',
    '__label__de': 'de',
    '__label__fr': 'fr',
    '__label__it': 'it',
    '__label__ja': 'ja',
    '__label__es': 'es',
    '__label__ceb': 'ceb',
    '__label__tr': 'tr',
    '__label__pt': 'pt',
    '__label__uk': 'uk',
    '__label__eo': 'eo',
    '__label__pl': 'pl',
    '__label__sv': 'sv',
    '__label__nl': 'nl',
    '__label__he': 'he',
    '__label__zh': 'zh',
    '__label__hu': 'hu',
    '__label__ar': 'ar',
    '__label__ca': 'ca',
    '__label__fi': 'fi',
    '__label__cs': 'cs',
    '__label__fa': 'fa',
    '__label__sr': 'sr',
    '__label__el': 'el',
    '__label__vi': 'vi',
    '__label__bg': 'bg',
    '__label__ko': 'ko',
    '__label__no': 'no',
    '__label__mk': 'mk',
    '__label__ro': 'ro',
    '__label__id': 'id',
    '__label__th': 'th',
    '__label__hy': 'hy',
    '__label__da': 'da',
    '__label__ta': 'ta',
    '__label__hi': 'hi',
    '__label__hr': 'hr',
    '__label__sh': 'sh',
    '__label__be': 'be',
    '__label__ka': 'ka',
    '__label__te': 'te',
    '__label__kk': 'kk',
    '__label__war': 'war',
    '__label__lt': 'lt',
    '__label__gl': 'gl',
    '__label__sk': 'sk',
    '__label__bn': 'bn',
    '__label__eu': 'eu',
    '__label__sl': 'sl',
    '__label__kn': 'kn',
    '__label__ml': 'ml',
    '__label__mr': 'mr',
    '__label__et': 'et',
    '__label__az': 'az',
    '__label__ms': 'ms',
    '__label__sq': 'sq',
    '__label__la': 'la',
    '__label__bs': 'bs',
    '__label__nn': 'nn',
    '__label__ur': 'ur',
    '__label__lv': 'lv',
    '__label__my': 'my',
    '__label__tt': 'tt',
    '__label__af': 'af',
    '__label__oc': 'oc',
    '__label__nds': 'nds',
    '__label__ky': 'ky',
    '__label__ast': 'ast',
    '__label__tl': 'tl',
    '__label__is': 'is',
    '__label__ia': 'ia',
    '__label__si': 'si',
    '__label__gu': 'gu',
    '__label__km': 'km',
    '__label__br': 'br',
    '__label__ba': 'ba',
    '__label__uz': 'uz',
    '__label__bo': 'bo',
    '__label__pa': 'pa',
    '__label__vo': 'vo',
    '__label__als': 'als',
    '__label__ne': 'ne',
    '__label__cy': 'cy',
    '__label__jbo': 'jbo',
    '__label__fy': 'fy',
    '__label__mn': 'mn',
    '__label__lb': 'lb',
    '__label__ce': 'ce',
    '__label__ug': 'ug',
    '__label__tg': 'tg',
    '__label__sco': 'sco',
    '__label__sa': 'sa',
    '__label__cv': 'cv',
    '__label__jv': 'jv',
    '__label__min': 'min',
    '__label__io': 'io',
    '__label__or': 'or',
    '__label__as': 'as',
    '__label__new': 'new'
}


def lang_id(text: str, ret_num: int = 1):
    fasttext_model = func_lang_detect_model.get_model()
    classification, confidence = fasttext_model.predict(text.replace("\n", " "), k=ret_num)
    result = {}
    for idx, val in enumerate(classification):
        new_label = __classification_language_map[classification[idx]]
        result[new_label] = confidence[idx]
    return result
