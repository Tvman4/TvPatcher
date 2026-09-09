package com.tvmods.tvpatcher;

interface IPatchService {
    void destroy() = 16777114;

    String copyObb();

    String restoreCache();
}
