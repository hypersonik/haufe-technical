package com.haufe.technical.api.utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;


@RequiredArgsConstructor
@NoArgsConstructor(force = true)
@Getter
public class RestResponsePage<T> {

    private final List<T> content;
    private final PageData page;

    public record PageData(int size, int number, long totalElements, long totalPages) {}
}
