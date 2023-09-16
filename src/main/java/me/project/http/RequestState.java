package me.project.http;

public enum RequestState {

    Preamble,
    Body,
    Expect,
    Complete
}
