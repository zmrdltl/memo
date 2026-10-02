package com.sparta.memo;

import com.sparta.memo.entity.Memo;
import com.sparta.memo.repository.MemoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TransactionRequiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TransactionTest {
    @PersistenceContext
    EntityManager em;

    @Autowired
    MemoRepository memoRepository;

    @Test
    @Transactional
    @Rollback(value = false) // 테스트 코드에서 @Transactional 를 사용하면 테스트가 완료된 후 롤백하기 때문에 false 옵션 추가
    @DisplayName("메모 생성 성공")
    void test1() {
        Memo memo = new Memo();
        memo.setUsername("Robbert");
        memo.setContents("@Transactional 테스트 중!");

        em.persist(memo);  // 영속성 컨텍스트에 메모 Entity 객체를 저장합니다.
        Long id = memo.getId();
        assertNotNull(id);
        em.flush();
        em.clear();
        Memo saved = em.find(Memo.class, id);
        assertEquals("Robbert", saved.getUsername());
        assertEquals("@Transactional 테스트 중!", saved.getContents());
    }

    @Test
    @DisplayName("메모 생성 실패")
    void test2() {
        Memo memo = new Memo();
        memo.setUsername("Robbie");
        memo.setContents("@Transactional 테스트 중!");

        assertThrows(TransactionRequiredException.class, () -> em.persist(memo));
    }

    @Test
    @Transactional
    @Rollback(value = false)
    @DisplayName("트랜잭션 전파 테스트")
    void test3() {
        Memo memo = new Memo();
        memo.setUsername("Robbert");
        memo.setContents("수정 전");
        em.persist(memo);
        Long id = memo.getId();

        Memo updated = memoRepository.findById(id).orElseThrow();
        updated.setUsername("Robbie");
        updated.setContents("@Transactional 전파 테스트 중!");
        assertSame(memo, updated);
        em.flush();
        em.clear();

        Memo saved = em.find(Memo.class, id);
        assertEquals("Robbie", saved.getUsername());
        assertEquals("@Transactional 전파 테스트 중!", saved.getContents());
    }
}
