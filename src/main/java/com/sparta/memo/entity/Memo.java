package com.sparta.memo.entity;

import com.sparta.memo.dto.MemoRequestDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Memo {
    private Long id;
    private String username;
    private String contents;

    public Memo(MemoRequestDto req) {
        this.username = req.getUsername();
        this.contents = req.getContents();
    }

    public void update(MemoRequestDto req) {
        this.username = req.getUsername();
        this.contents = req.getContents();
    }
}