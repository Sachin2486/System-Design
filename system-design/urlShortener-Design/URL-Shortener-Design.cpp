#include <bits/stdc++.h>
using namespace std;

class URLShortener{
    private:
    unordered_map<string,string> urlToShorten;
    unordered_map<string,string> shortenToURL;
    const string chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    const int shortURLLength = 6;

    public:
    string generateShortURL(const std::string &url)
    {
        std::string shortURL = generateRandomShortURL();
        while(shortenToURL.find(shortURL) != shortenToURL.end()){
            shortURL = generateRandomShortURL();
        }

        urlToShorten[url] = shortURL;
        shortenToURL[shortURL] = url;
        return shortURL;
    }

    string getOriginalURL(const std::string &shortURL)
    {
        if(shortenToURL.find(shortURL) != shortenToURL.end()){
            return shortenToURL[shortURL];
        }

        return "Shorten URL not found";
    }

    private:
    std :: string generateRandomShortURL()
    {
        std :: string shortURL;
        int len = chars.size();
        srand(time(0));
        for(int i=0;i<shortURLLength;++i){
            shortURL.push_back(chars[rand() % len]);
        
        }
        return shortURL;
    }
};

int main(){
    URLShortener urlShortener;

   string longURL1 = "https://www.example.com/page1";
    string shortURL1 = urlShortener.generateShortURL(longURL1);
    cout << "Generated Short URL: " << shortURL1 << "\n";

    string longURL2 = "https://www.example.com/page2";
    string shortURL2 = urlShortener.generateShortURL(longURL2);
    cout << "Generated Short URL: " << shortURL2 << "\n";

    cout << "Original URL for " << shortURL1 << ": " << urlShortener.getOriginalURL(shortURL1) << "\n";
    cout << "Original URL for " << shortURL2 << ": " << urlShortener.getOriginalURL(shortURL2) << "\n";

    return 0;

};