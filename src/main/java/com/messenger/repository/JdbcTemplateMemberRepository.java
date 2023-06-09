package com.messenger.repository;

import com.messenger.domain.Member;
import com.messenger.domain.MemberRole;
import com.messenger.exception.ErrorCode;
import com.messenger.exception.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
public class JdbcTemplateMemberRepository implements MemberRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTemplateMemberRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private RowMapper<Member> memberRowMapper() {
        return (rs, rowNum) -> Member.builder()
                .id(rs.getString("id"))
                .password(rs.getString("pw"))
                .name(rs.getString("display_name"))
                .statusMessage(rs.getString("status_message"))
                .role(MemberRole.valueOf(rs.getString("role")))
                .build();
    }

    public Member save(Member member) {
        String sql = "INSERT INTO member(id, pw, display_name, status_message) values(?, ?, ?, ?)";
        log.debug("member={}", member);
        Object[] args = {
                member.getId(),
                member.getPassword(),
                member.getName(),
                member.getStatusMessage()
        };
        try {
            jdbcTemplate.update(sql, args);
        } catch (DuplicateKeyException e) {
            throw new MyException(ErrorCode.ALREADY_EXIST_ID);
        }
        return member;
    }

    public Optional<Member> findById(String id) {
        String sql = "SELECT * FROM member WHERE id = ?";
        List<Member> result = jdbcTemplate.query(sql, memberRowMapper(), id);
        return result.stream().findAny();
    }

    @Override
    public List<Member> findByName(String name) {
        String sql = "SELECT * FROM member WHERE display_name = ?";
        return jdbcTemplate.query(sql, memberRowMapper(), name);
    }

    @Override
    public List<Member> findAll() {
        String sql = "SELECT * FROM member";
        return jdbcTemplate.query(sql, memberRowMapper());
    }

    @Override
    public Optional<Member> findByIdAndPw(String id, String password) {
        String sql = "SELECT * FROM member WHERE id = ? AND pw = ?";
        List<Member> result = jdbcTemplate.query(sql, memberRowMapper(), id, password);
        return result.stream().findAny();
    }

    @Override
    public Member updateMember(Member paramMember) {
        String sql = "UPDATE member SET pw = ?, display_name = ?, status_message = ? WHERE id = ?";
        log.debug("paramMember={}", paramMember);
        Object[] args = {
                paramMember.getPassword(),
                paramMember.getName(),
                paramMember.getStatusMessage(),
                paramMember.getId()
        };
        int update = jdbcTemplate.update(sql, args);
        log.debug("update={}", update);
        if (update == 0) {
            throw new MyException(ErrorCode.NOT_MODIFIED);
        }
        return findById(paramMember.getId()).orElseThrow(() -> new MyException(ErrorCode.NOT_FOUND_MEMBER));
    }
}
