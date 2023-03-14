package com.maturi.onetaek;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
@SpringBootTest
@Transactional
public class QuerydslTest {

    @PersistenceContext
    EntityManager em;

    @Test
    @DisplayName("querydsl 작동하는지 확인")
    void querydslTest(){
//
    }
}
