#ifndef BOOK_H_INCLUDED
#define BOOK_H_INCLUDED

class Book{
public:
  string author;
  string category;
  string status;
  short pages;
  Book() : Book("Noname","Nocategory","Nostatus",0) {};
  Book(string a, string c, string s, short p) : author(a), category(c), status(s) , pages(p){};
  Book(const Book & bk){
    author = bk.author;
    category = bk.category;
    status = bk.status;
    pages = bk.pages;
  }
  Book & operator=(const Book & bk){
    if ( this == &bk )
      return *this;
    author = bk.author;
    category = bk.category;
    status = bk.status;
    pages = bk.pages;
    return *this;
  }
  friend std::ostream& operator<<(std::ostream& os,const Book &bk){
    std::cout << " author: "<< bk.author << "   category: " << bk.category << "   status: " << bk.status << "   pages: "<< bk.pages;
    return os;
  }
};

#endif /* BOOK_H_INCLUDED */
