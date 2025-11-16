import torch
import numpy as np
from sklearn.preprocessing import normalize
import torchreid

device = "cuda" if torch.cuda.is_available() else "cpu"

reid_model = torchreid.models.build_model(
    name="osnet_x1_0",
    num_classes=1000,
    pretrained=True
).eval().to(device)

def extract_reid_feature(img_tensor):
    with torch.no_grad():
        feat = reid_model(img_tensor)
        feat = feat.view(feat.size(0), -1)
        feat = normalize(feat.cpu().numpy(), axis=1)
    return feat
