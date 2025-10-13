package filereader

func (f *FileReader) appendLinestoResult(reversedBytes []byte) string {
	result := ""
	for i := 0; i < len(reversedBytes); i++ {
		result += string(reversedBytes[i])
	}
	return result
}

func (f *FileReader) reverseResult(fileLines []string) []string {
	if len(fileLines) == 1 {
		return fileLines
	}

	reverseFileLines := make([]string, len(fileLines))

	idx := 0
	for i := len(fileLines) - 1; i >= 0; i-- {
		reverseFileLines[idx] = fileLines[i]
		idx++
	}
	return reverseFileLines
}
