import zipfile
import re
import sys

path = r"c:\Users\xiaoyuzi\Desktop\文件\面试\金冠宇简历（第一段实习后）(5)_STAR改.docx"

with zipfile.ZipFile(path) as z:
    with z.open("word/document.xml") as f:
        xml = f.read().decode("utf-8")

# Extract text between <w:t...>...</w:t> tags, and add newline on paragraph breaks
text_parts = []
for para in re.split(r"</w:p>", xml):
    texts = re.findall(r"<w:t[^>]*>(.*?)</w:t>", para, re.DOTALL)
    line = "".join(texts)
    line = line.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
    text_parts.append(line)

with open(r"D:\java\my-lesson\_resume_extract.txt", "w", encoding="utf-8") as out:
    out.write("\n".join(text_parts))

print("done, lines:", len(text_parts))
