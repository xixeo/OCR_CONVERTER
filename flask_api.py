from flask import Flask, request, jsonify
from paddleocr import PaddleOCR
import cv2
import os
import uuid
import threading
import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"


app = Flask(__name__)

# PaddleOCR 객체는 한 번만 생성
ocr_model = PaddleOCR(lang='korean')
ocr_lock = threading.Lock()

@app.route('/paddleocr', methods=['POST'])
def ocr_images():
    try:
        files = request.files.getlist("images")
        if not files:
            return jsonify({"error": "No images uploaded"}), 400

        print(f"📂 업로드된 파일 목록: {[f.filename for f in files]}")
        results = {}

        for idx, file in enumerate(files, 1):
            filename = file.filename
            print(f"🔄 OCR 수행 중... {filename}")

            image_bytes = file.read()
            temp_filename = f"temp_{uuid.uuid4()}.jpg"
            os.makedirs("temp", exist_ok=True)
            temp_path = os.path.join("temp", temp_filename)

            with open(temp_path, "wb") as f:
                f.write(image_bytes)

            image = cv2.imread(temp_path)
            os.remove(temp_path)

            if image is None:
                print(f"❌ 이미지 로드 실패: {filename}")
                results[filename] = []
                continue

            with ocr_lock:
                result = ocr_model.ocr(image)

            vision_format = []
            full_text = ""

            for line in result[0]:
                text = line[1][0]
                full_text += text + "\n"
                box = line[0]
                bounding_poly = [{"x": int(x), "y": int(y)} for (x, y) in box]
                vision_format.append({"text": text, "boundingPoly": bounding_poly})

            all_points = [pt for item in vision_format for pt in item["boundingPoly"]]
            if all_points:
                x_min = min(p["x"] for p in all_points)
                x_max = max(p["x"] for p in all_points)
                y_min = min(p["y"] for p in all_points)
                y_max = max(p["y"] for p in all_points)

                full_text_item = {
                    "text": full_text.strip(),
                    "boundingPoly": [
                        {"x": x_min, "y": y_min},
                        {"x": x_max, "y": y_min},
                        {"x": x_max, "y": y_max},
                        {"x": x_min, "y": y_max}
                    ]
                }

                results[filename] = [full_text_item] + vision_format
            else:
                results[filename] = []

        print(f"{filename} 이미지 OCR 처리 완료")
        return jsonify(results)

    except Exception as e:
        import traceback
        traceback.print_exc()
        print("❌ OCR 처리 중 오류:", str(e))
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("🚀 PaddleOCR Flask 서버 시작 중...")
    from waitress import serve
    serve(app, host="0.0.0.0", port=5999)