package filereader

import (
	"bufio"
	"io"
	"os"
)

type FileReader struct {
	filePath string
	lastByte int64
}

func (f *FileReader) ReadLast10Lines() ([]string, error) {

	file, err := os.Open(f.filePath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	fileStat, err := file.Stat()
	if err != nil {
		return nil, err
	}

	fileSize := fileStat.Size()

	var fileLines []string
	buf := make([]byte, 1)
	reverseBytes := make([]byte, 0)

	filePosition := fileSize - 1

	for filePosition >= f.lastByte && len(fileLines) < 10 {
		_, err := file.Seek(filePosition, io.SeekStart)

		if err != nil {
			return nil, err
		}

		_, err = file.Read(buf)
		if err != nil {
			return nil, err
		}

		if buf[0] == '\n' {
			fileLines = append(fileLines, f.appendLinestoResult(reverseBytes))
			reverseBytes = []byte{}
		} else {
			reverseBytes = append([]byte{buf[0]}, reverseBytes...)
		}
		filePosition--
	}
	f.lastByte = fileSize

	if len(fileLines) < 10 && filePosition < 0 {
		fileLines = append(fileLines, f.appendLinestoResult(reverseBytes))
	}

	return f.reverseResult(fileLines), nil
}

func (f *FileReader) ReadNewLines() ([]string, error) {
	file, err := os.Open(f.filePath)
	if err != nil {
		return nil, err
	}

	defer file.Close()

	stats, err := file.Stat()
	if err != nil {
		return nil, err
	}

	size := stats.Size()

	file.Seek(f.lastByte, io.SeekStart)
	fileScanner := bufio.NewScanner(file)
	fileScanner.Split(bufio.ScanLines)
	var fileLines []string

	for fileScanner.Scan() {
		fileLines = append(fileLines, fileScanner.Text())
	}

	f.lastByte = size

	return fileLines, nil
}

func NewFileReader(filePath string) *FileReader {
	return &FileReader{
		filePath: filePath,
		lastByte: 0,
	}
}
