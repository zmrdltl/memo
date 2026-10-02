package com.sparta.memo;

import com.sparta.memo.entity.Memo;
import com.sparta.memo.repository.MemoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemoControllerTests {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemoRepository memoRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @PersistenceContext
    EntityManager em;

    @Test
    @DisplayName("내용에 키워드를 포함하는 메모만 수정 시간 내림차순으로 조회한다")
    void getMemosByKeyword() throws Exception {
        String keyword = UUID.randomUUID().toString();
        Memo latest = saveMemo("작성자", "앞 " + keyword + " 뒤");
        Memo oldest = saveMemo("작성자", keyword + " 포함");
        saveMemo(keyword, "작성자만 일치하는 메모");

        setModifiedAt(latest.getId(), LocalDateTime.of(2026, 1, 2, 12, 0));
        setModifiedAt(oldest.getId(), LocalDateTime.of(2026, 1, 1, 12, 0));
        em.clear();

        mockMvc.perform(get("/api/memos/contents").param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(latest.getId()))
                .andExpect(jsonPath("$[1].id").value(oldest.getId()))
                .andExpect(jsonPath("$[0].modifiedAt").isNotEmpty());
    }

    @Test
    @DisplayName("LIKE 와일드카드도 일반 문자로 검색한다")
    void searchLiteralWildcardCharacters() throws Exception {
        String prefix = UUID.randomUUID().toString();
        Memo literal = saveMemo("작성자", prefix + "%_");
        saveMemo("작성자", prefix + "xy");

        mockMvc.perform(get("/api/memos/contents").param("keyword", prefix + "%_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(literal.getId()));
    }

    @Test
    @DisplayName("일치하는 메모가 없으면 빈 목록을 반환한다")
    void returnEmptyListWhenNoMemoMatches() throws Exception {
        mockMvc.perform(get("/api/memos/contents").param("keyword", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("검색 키워드 Query String은 필수다")
    void requireKeyword() throws Exception {
        mockMvc.perform(get("/api/memos/contents"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API로 생성·수정·삭제한 메모가 DB와 검색 결과에 반영된다")
    void persistMemoThroughCrudEndpoints() throws Exception {
        String keyword = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"작성자","contents":"%s"}
                                """.formatted(keyword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.modifiedAt").isNotEmpty());

        Memo memo = memoRepository.findByContentsContainingOrderByModifiedAtDesc(keyword).get(0);
        Long id = memo.getId();
        LocalDateTime previousModifiedAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        setModifiedAt(id, previousModifiedAt);
        em.clear();

        mockMvc.perform(put("/api/memos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"수정한 작성자","contents":"수정 %s"}
                                """.formatted(keyword)))
                .andExpect(status().isOk())
                .andExpect(content().string(id.toString()));

        em.flush();
        em.clear();
        Memo updated = memoRepository.findById(id).orElseThrow();
        assertEquals("수정한 작성자", updated.getUsername());
        assertEquals("수정 " + keyword, updated.getContents());
        assertTrue(updated.getModifiedAt().isAfter(previousModifiedAt));

        mockMvc.perform(get("/api/memos/contents").param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contents").value("수정 " + keyword));

        mockMvc.perform(delete("/api/memos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().string(id.toString()));

        mockMvc.perform(get("/api/memos/contents").param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        assertFalse(memoRepository.existsById(id));
    }

    private Memo saveMemo(String username, String contents) {
        Memo memo = new Memo();
        memo.setUsername(username);
        memo.setContents(contents);
        return memoRepository.saveAndFlush(memo);
    }

    private void setModifiedAt(Long id, LocalDateTime modifiedAt) {
        jdbcTemplate.update("UPDATE memo SET modified_at = ? WHERE id = ?", Timestamp.valueOf(modifiedAt), id);
    }
}
