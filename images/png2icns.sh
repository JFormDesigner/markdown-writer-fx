mkdir markdown-writer-fx.iconset
cp markdown-writer-fx-32.png markdown-writer-fx.iconset/icon_32x32.png
cp markdown-writer-fx-64.png markdown-writer-fx.iconset/icon_64x64.png
cp markdown-writer-fx-128.png markdown-writer-fx.iconset/icon_128x128.png
cp markdown-writer-fx-256.png markdown-writer-fx.iconset/icon_256x256.png
cp markdown-writer-fx-512.png markdown-writer-fx.iconset/icon_512x512.png

iconutil --convert icns markdown-writer-fx.iconset

rm markdown-writer-fx.iconset/*
rmdir markdown-writer-fx.iconset
