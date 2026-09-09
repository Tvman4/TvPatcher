package com.tvmods.tvpatcher;

interface IPatchService {
    void destroy() = 16777114;
    String copyObb() = 1;
    String restoreCache() = 2;
}
