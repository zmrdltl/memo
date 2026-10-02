package com.sparta.memo;

import com.sparta.memo.entity.Memo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EntityTest {
    @Autowired
    private EntityManagerFactory emf;

    private EntityManager em;

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
        em.getTransaction().begin();
    }

    @AfterEach
    void tearDown() {
        try {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
        } finally {
            em.close();
        }
    }

    @Test
    @DisplayName("1차 캐시 : Entity 저장")
    void test1() {
        Memo memo = persistMemo("Robbie", "1차 캐시 Entity 저장");

        assertSame(memo, em.find(Memo.class, memo.getId()));

        em.flush();
        em.clear();
        Memo saved = em.find(Memo.class, memo.getId());
        assertEquals("Robbie", saved.getUsername());
        assertEquals("1차 캐시 Entity 저장", saved.getContents());
    }

    @Test
    @DisplayName("준영속 상태 : detach()")
    void test2() {
        Memo memo = persistMemo("Robbie", "수정 전");
        em.flush();
        assertTrue(em.contains(memo));

        em.detach(memo);
        assertFalse(em.contains(memo));
        memo.setUsername("Update");
        memo.setContents("준영속 객체 수정");
        em.flush();

        Memo saved = em.find(Memo.class, memo.getId());
        assertNotSame(memo, saved);
        assertEquals("Robbie", saved.getUsername());
        assertEquals("수정 전", saved.getContents());
    }

    @Test
    @DisplayName("준영속 상태 : clear()")
    void test3() {
        Memo memo1 = persistMemo("Robbie", "첫 번째 메모");
        Memo memo2 = persistMemo("Robbert", "두 번째 메모");
        em.flush();
        assertTrue(em.contains(memo1));
        assertTrue(em.contains(memo2));

        em.clear();
        assertFalse(em.contains(memo1));
        assertFalse(em.contains(memo2));
        memo1.setContents("준영속 객체 수정");

        Memo memo = em.find(Memo.class, memo1.getId());
        assertNotSame(memo1, memo);
        assertTrue(em.contains(memo));
        assertEquals("첫 번째 메모", memo.getContents());
        memo.setUsername("Update");
        memo.setContents("다시 조회한 객체 수정");
        em.flush();
        em.clear();

        Memo updated = em.find(Memo.class, memo1.getId());
        assertEquals("Update", updated.getUsername());
        assertEquals("다시 조회한 객체 수정", updated.getContents());
    }

    @Test
    @DisplayName("객체 동일성 보장")
    void test4() {
        Memo first = persistMemo("Robbie", "첫 번째 메모");
        Memo second = persistMemo("Robbert", "객체 동일성 보장");
        em.flush();
        em.clear();

        Memo memo1 = em.find(Memo.class, first.getId());
        Memo memo2 = em.find(Memo.class, first.getId());
        Memo memo3 = em.find(Memo.class, second.getId());

        assertNotNull(memo1);
        assertNotNull(memo3);
        assertSame(memo1, memo2);
        assertNotSame(memo1, memo3);
    }

    @Test
    @DisplayName("Entity 삭제")
    void test5() {
        Memo initial = persistMemo("Robbert", "삭제할 메모");
        em.flush();
        em.clear();

        Memo memo = em.find(Memo.class, initial.getId());
        em.remove(memo);
        em.flush();
        em.clear();

        assertNull(em.find(Memo.class, initial.getId()));
    }

    @Test
    @DisplayName("쓰기 지연 저장소 확인 : UPDATE")
    void test6() {
        Memo memo1 = persistMemo("Robbert", "수정 전 1");
        Memo memo2 = persistMemo("Bob", "수정 전 2");

        // IDENTITY는 INSERT를 즉시 실행하므로 UPDATE의 쓰기 지연을 확인한다.
        memo1.setContents("쓰기 지연 저장소");
        memo2.setContents("과연 저장을 잘 하고 있을까?");
        assertEquals("수정 전 1", getStoredContents(memo1.getId()));
        assertEquals("수정 전 2", getStoredContents(memo2.getId()));
        em.flush();
        assertEquals("쓰기 지연 저장소", getStoredContents(memo1.getId()));
        assertEquals("과연 저장을 잘 하고 있을까?", getStoredContents(memo2.getId()));
    }

    @Test
    @DisplayName("flush() 메서드 확인")
    void test7() {
        Memo memo = persistMemo("Flush", "flush 전");
        memo.setContents("Flush() 메서드 호출");

        em.flush();
        em.clear();

        Memo saved = em.find(Memo.class, memo.getId());
        assertNotNull(saved);
        assertNotSame(memo, saved);
        assertEquals("Flush() 메서드 호출", saved.getContents());
    }

    @Test
    @DisplayName("변경 감지 확인")
    void test8() {
        Memo initial = persistMemo("Flush", "수정 전");
        em.flush();
        em.clear();

        Memo memo = em.find(Memo.class, initial.getId());
        memo.setUsername("Update");
        memo.setContents("변경 감지 확인");
        em.flush();
        em.clear();

        Memo updated = em.find(Memo.class, initial.getId());
        assertEquals("Update", updated.getUsername());
        assertEquals("변경 감지 확인", updated.getContents());
    }

    private Memo persistMemo(String username, String contents) {
        Memo memo = new Memo();
        memo.setUsername(username);
        memo.setContents(contents);
        em.persist(memo);
        return memo;
    }

    private String getStoredContents(Long id) {
        return (String) em.createNativeQuery("select contents from memo where id = :id")
                .setParameter("id", id)
                .setFlushMode(FlushModeType.COMMIT)
                .getSingleResult();
    }
}
