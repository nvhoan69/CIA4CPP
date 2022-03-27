find . -type f -not \( -name '*.c' -or -name '*.h' -or -name '*.cc' -or -name '*.hh' -or -name '*.cpp' -or -name '*.hpp' -or -name '*.c++' -or -name '*.h++' -or -name '*.cxx' -or -name '*.hxx' \) -print -delete
find . -type d -empty -print -delete
