package com.sparta.memo.service;

import com.sparta.memo.dto.MemoRequestDto;
import com.sparta.memo.dto.MemoResponseDto;
import com.sparta.memo.entity.Memo;
import com.sparta.memo.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemoService {
    private final MemoRepository memoRepository;

    public MemoResponseDto createMemo(MemoRequestDto requestDto) {
        Memo memo = memoRepository.save(new Memo(requestDto));
        return new MemoResponseDto(memo);
    }

    @Transactional(readOnly = true)
    public List<MemoResponseDto> getMemos() {
        return memoRepository.findAllByOrderByModifiedAtDesc().stream()
                .map(MemoResponseDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemoResponseDto> getMemosByKeyword(String keyword) {
        return memoRepository.findByContentsContainingOrderByModifiedAtDesc(keyword).stream()
                .map(MemoResponseDto::new)
                .toList();
    }

    public Long updateMemo(Long id, MemoRequestDto requestDto) {
        Memo memo = findMemo(id);
        memo.update(requestDto);
        return memo.getId();
    }

    public Long deleteMemo(Long id) {
        Memo memo = findMemo(id);
        memoRepository.delete(memo);
        return id;
    }

    private Memo findMemo(Long id) {
        return memoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("선택한 메모는 존재하지 않습니다."));
    }
}
