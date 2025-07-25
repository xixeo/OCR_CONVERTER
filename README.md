## 1. 시스템 전체 구조

| 구성 요소 | 설명 |
| --- | --- |
| **Spring Boot (Java)** | 업로드 처리, 상태 추적, OCR 요청, JSON/PDF 저장 |
| **Flask (Python)** | PaddleOCR 수행 |

---

## 2. 서비스 흐름 요약

![image.png](attachment:33ce9e8b-168e-48c2-a92f-f9d2f0f5aeed:image.png)

---

## 3. 재처리 흐름 요약

```
[사용자] ▶ 'OCR 변환 다시 시작' 버튼 클릭
   ↓
[Spring Controller] ▶ /upload/restart-ocr 호출
   ↓
[시스템]
  1. UUID 확인 (3days 유효)
  2. 업로드된 이미지 목록 수집
  3. OCR 결과(status.json) 누락 파일 식별
  4. 누락된 이미지만 OCR 재요청
  5. PDF 재생성 시도 (결과 수 확인 후 진행)
   ↓
[사용자] ▶ "PDF 생성 완료" 또는 "OCR 중 오류" 메시지 수신
```

---

## 4. 최종 PDF 결과 저장 구조

```
서버경로
  Ͱ── {UUID}
  |    └── {사용자_업로드_폴더}
  |        ├── 이미지.jpg
  |        └── 하위폴더/이미지.jpg
  |
  └── ocr_results
       └── {UUID}
           └── {사용자_업로드_폴더}
               ├── 이미지.json, {사용자_업로드_폴더}.pdf
               └── 하위폴더/이미지.json, 하위폴더.pdf
```

- OCR 결과: 각 이미지에 대해 `.json` 파일 생성
- PDF 결과: 디렉토리 단위로 `.pdf` 병합 저장

**4-1. 서버 폴더 구조**


```bash
  /서버
  
  Ͱ── lif-fv-cnvr-28e48cfee7e9.json (GoogleVision Key)
  |
  Ͱ── ocr-app
  |    └── springboot jar
  |
  Ͱ── flask-app
  |    └── python 및 flask 파일
  |
  Ͱ── test
  |    Ͱ── {UUID}
  |    |    └── {사용자_업로드_폴더}
  |    |        ├── 이미지.jpg
  |    |        └── 하위폴더/이미지.jpg
  |    |
  |    └── ocr_results
  |        └── {UUID}
  |            └── {사용자_업로드_폴더}
  |                ├── 이미지.json, {사용자_업로드_폴더}.pdf
  |                └── 하위폴더/이미지.json, 하위폴더.pdf
  └── status
      └── {UUID}
          └── status.json
```
