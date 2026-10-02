package com.sparta.memo.entity;

import com.sparta.memo.dto.MemoRequestDto;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Memo {
    @Id
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
