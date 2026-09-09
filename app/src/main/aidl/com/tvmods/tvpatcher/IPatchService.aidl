package com.tvmods.tvpatcher;

interface IPatchService {

    String copyObb();

    String restoreCache();

    boolean isReady();
}
