package com.mars.app.domain.community.service;

import static com.mars.common.exception.NPGExceptionType.NOT_FOUND_COMMUNITY;
import static com.mars.common.exception.NPGExceptionType.NOT_FOUND_USER;
import static com.mars.common.exception.NPGExceptionType.UNAUTHORIZED_NO_AUTHENTICATION_CONTEXT;

import com.mars.app.domain.community.dto.CommunityListResponseDto;
import com.mars.common.dto.page.PageResponseDto;
import com.mars.app.domain.community.repository.CommunityCommentRepository;
import com.mars.app.domain.community.dto.CommunityRequestDto;
import com.mars.app.domain.community.dto.CommunityResponseDto;
import com.mars.common.dto.page.PageRequestVO;
import com.mars.common.model.community.Community;
import com.mars.app.domain.community.repository.CommunityLikeRepository;
import com.mars.app.domain.community.repository.CommunityRepository;
import com.mars.app.domain.firebase.service.FirebaseStorageService;
import com.mars.common.model.community.CommunityLike;
import com.mars.common.model.user.User;
import com.mars.app.domain.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityLikeRepository communityLikeRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final UserRepository userRepository;
    private final FirebaseStorageService firebaseStorageService;

    public CommunityResponseDto getCommunityById(Long id, Long userId) {
        Community community = getCommunity(id);

        if (community.isPrivate()) {
            validateOwnership(community, userId);
        }

        return CommunityResponseDto.of(community, userId);
    }

    public PageResponseDto<CommunityListResponseDto> pagesByCommunity(Long userId, PageRequestVO pageRequestVO) {
        List<CommunityLike> communityLikes = getCommunityLikesBy(userId);

        return PageResponseDto.of((communityRepository.findByIsPublicTrueOrUserId(userId, pageRequestVO.toPageable()))
                .map(community -> {
                    int likeCount = communityLikeRepository.countByCommunityId(community.getId());
                    int commentCount = communityCommentRepository.countByCommunityId(community.getId());
                    return CommunityListResponseDto.of(community, likeCount, commentCount, communityLikes);
                })
        );
    }

    public CommunityResponseDto getPostForEdit(Long id, Long userId) {
        Community community = getCommunity(id);
        validateOwnership(community, userId);

        return CommunityResponseDto.of(community, userId);
    }

    @Transactional
    public CommunityResponseDto createCommunity(CommunityRequestDto requestDto, MultipartFile file, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER::of);

        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            imageUrl = firebaseStorageService.uploadNewFile(file);
        }

        Community community = Community.of(
            user,
            requestDto.title(),
            requestDto.content(),
            imageUrl,
            requestDto.isPublic()
        );

        communityRepository.save(community);
        return CommunityResponseDto.of(community, userId);
    }

    @Transactional
    public CommunityResponseDto updateCommunity(Long id, CommunityRequestDto requestDto, MultipartFile file, Long userId) {
        Community community = getCommunity(id);
        validateOwnership(community, userId);

        community.update(
            requestDto.title(),
            requestDto.content(),
            requestDto.isPublic(),
            getUpdatedImageUrl(file, community.getImageUrl())
        );

        return CommunityResponseDto.of(community, userId);
    }

    @Transactional
    public void deleteCommunity(Long id, Long userId) {
        Community community = getCommunity(id);
        validateOwnership(community, userId);
        firebaseStorageService.deleteFileFromFirebase(community.getImageUrl());
        communityRepository.deleteById(id);
    }

    private Community getCommunity(Long id) {
        return communityRepository.findById(id)
            .orElseThrow(NOT_FOUND_COMMUNITY::of);
    }

    private void validateOwnership(Community community, Long userId) {
        if (!community.getUser().getId().equals(userId)) {
            throw UNAUTHORIZED_NO_AUTHENTICATION_CONTEXT.of("게시물을 수정/삭제할 권한이 없습니다.");
        }
    }

    private List<CommunityLike> getCommunityLikesBy(Long userId) {
        return userId.equals(User.ANONYMOUS_USER_ID)
            ? new ArrayList<>()
            : communityLikeRepository.findCommunityLikesByUserId(userId);
    }

    private String getUpdatedImageUrl(MultipartFile file, String previousImageUrl) {
        if (file == null || file.isEmpty()) {
            return previousImageUrl;
        }
        return firebaseStorageService.updateFile(file, previousImageUrl);
    }

}
