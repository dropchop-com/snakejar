from typing import Any, Dict, List


def params_hashmap(lang: str, docs: Dict[str, Any]):
    results = []
    for key in docs:
        results.append(key + ":=" + docs[key])

    return results


def params_list(lang: str, docs: List[Any]):
    results = []
    for idx, doc in enumerate(docs):
        results.append(str(idx) + ":=" + doc)

    return results


def params_list_hashmap(lang: str, docs: List[Dict[str, Any]]):
    results = []
    for idx, doc in enumerate(docs):
        result = {'title': doc['title'], 'body': doc['body'], 'lang': lang, 'embed': []}
        for idx2, num in enumerate(doc['embed']):
            result['embed'].append(num * idx2)
        result['embed2'] = [3.1, 3.2, 3.3, 3.4, 3.12324324]
        results.append(result)

    return results
