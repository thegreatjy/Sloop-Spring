package kr.co.sloop.sample.service.impl;

import kr.co.sloop.sample.domain.MemberDTO;

public interface MemberService {
    int signup(MemberDTO memberDTO);

    boolean login(MemberDTO memberDTO);

    String emailCheck(String memberEmail);
}
