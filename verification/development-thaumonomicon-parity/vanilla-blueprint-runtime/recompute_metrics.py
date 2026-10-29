"""Read-only reproduction of the retained diagnostic crop metrics (Pillow, NumPy)."""
from pathlib import Path
from PIL import Image
import numpy as np,json
root=Path(__file__).resolve().parent
report=json.loads((root/'comparison.json').read_text())
for item in report['comparisons']:
 a=np.asarray(Image.open(root/item['original']).convert('RGB').crop(item['crop_xyxy']),dtype=np.int16)
 b=np.asarray(Image.open(root/item['modern']).convert('RGB').crop(item['crop_xyxy']),dtype=np.int16)
 ma=a.max(axis=2)<140;mb=b.max(axis=2)<140;common=ma&mb
 mae=float(np.abs(a-b)[common].mean());iou=float(common.sum()/(ma|mb).sum())
 assert abs(mae-item['mean_absolute_rgb_channel_error_on_common_dark_pixels'])<1e-12
 assert abs(iou-item['dark_threshold_mask_iou'])<1e-12
 print(item['blueprint'], 'MAE',mae,'mask IoU',iou)
