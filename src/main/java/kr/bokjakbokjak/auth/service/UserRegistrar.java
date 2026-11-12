package kr.bokjakbokjak.auth.service;

import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 회원 생성을 별도 트랜잭션으로 분리해 동시 첫 로그인 충돌을 바깥에서 처리할 수 있게 한다. */
@Component
class UserRegistrar {
    private final UserRepository userRepository;

    UserRegistrar(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User register(Long universityId, String googleSub, String email) {
        return userRepository.saveAndFlush(User.register(universityId, googleSub, email));
    }
}
