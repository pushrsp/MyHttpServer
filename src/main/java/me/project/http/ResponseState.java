package me.project.http;

public enum ResponseState {

    Preamble,
    Body,
    KeepAlive,
    Close,
    Expect,
    Continue
}
