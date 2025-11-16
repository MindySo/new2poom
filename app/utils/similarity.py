from sklearn.metrics.pairwise import cosine_similarity

def calc_similarity(features, query_feature):
    return cosine_similarity(features, query_feature)
