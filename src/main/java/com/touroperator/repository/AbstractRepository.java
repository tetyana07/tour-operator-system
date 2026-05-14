package com.touroperator.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;


public abstract class AbstractRepository<T> {

    protected final JdbcTemplate jdbcTemplate;

    public AbstractRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    protected void save(String sql, Object... params) {
        jdbcTemplate.update(sql, params);
    }

    protected T findById(String sql, RowMapper<T> mapper, Object... params) {
        try {
            return jdbcTemplate.queryForObject(sql, mapper, params);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    protected List<T> findAll(String sql, RowMapper<T> mapper) {
        return jdbcTemplate.query(sql, mapper);
    }

    protected void delete(String sql, Object... params) {
        jdbcTemplate.update(sql, params);
    }
}
