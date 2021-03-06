package com.kh.wheregarden.domain.board.svc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

//import com.kh.wheregarden.domain.board.FileStore;
import com.kh.wheregarden.domain.board.dao.BoardDAO;
import com.kh.wheregarden.domain.board.dto.BoardDTO;
import com.kh.wheregarden.domain.board.dto.SearchDTO;
import com.kh.wheregarden.domain.common.dao.UpLoadFileDAO;
import com.kh.wheregarden.domain.common.dto.UpLoadFileDTO;
import com.kh.wheregarden.domain.common.file.FileStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardSVCImpl implements BoardSVC{

	private final BoardDAO boardDAO;
	private final UpLoadFileDAO upLoadFileDAO;
	private final FileStore fileStore;

	//원글작성
	@Override
	public Long boardWrite(BoardDTO boardDTO) {
		//게시글작성
		Long bnum = boardDAO.boardWrite(boardDTO);

		//첨부파일 메타정보 저장
		upLoadFileDAO.addFiles(
				convert(bnum, boardDTO.getBcategory(), boardDTO.getFiles())
		);
		return bnum;
	}

	private List<UpLoadFileDTO> convert(
			Long bnum,String bcategory,List<UpLoadFileDTO> files) {
		for(UpLoadFileDTO ele : files) {
			ele.setRid(String.valueOf(bnum));
			//ele.setCode(bcategory);
		}
		return files;
	}

	//답글 작성
	@Override
	public Long replyWrite(BoardDTO boardDTO) {
		
		Long bnum = boardDAO.replyWrite(boardDTO);
		
		//첨부파일 메타정보 저장
		upLoadFileDAO.addFiles(
				convert(bnum, boardDTO.getBcategory(), boardDTO.getFiles())
		);		
		return bnum;
	}

	//게시글 수정
	@Override
	public Long boardModify(Long bnum, BoardDTO boardDTO) {
		Long modifiedBnum = boardDAO.boardModify(bnum, boardDTO);
		//첨부파일 메타정보 저장
		upLoadFileDAO.addFiles(
				convert(bnum, boardDTO.getBcategory(), boardDTO.getFiles())
		);
		return modifiedBnum;
	}

	//게시글 삭제
	@Override
	public void boardDel(Long bnum) {
		//서버파일 시스템에 있는 업로드 파일삭제
		fileStore.deleteFiles(upLoadFileDAO.getStore_Fname(String.valueOf(bnum)));
		//업로드 파일 메타정보 삭제
		upLoadFileDAO.deleteFileByRid(String.valueOf(bnum));
		//게시글 삭제
		boardDAO.boardDel(bnum);
				
	}

	//카테고리별 게시글 리스트
	@Override
	public List<BoardDTO> list(String bcategory, int startRec, int endRec) {
		
		return boardDAO.list(bcategory, startRec, endRec);
	}
	//게시판 카테고리별 검색결과 목록
	@Override
	public List<BoardDTO> list(SearchDTO searchDTO) {
		List<BoardDTO> list = boardDAO.list(searchDTO);
		return list;
	}

	//게시글 상세
	@Override
	public BoardDTO boardDetail(Long bnum) {
		//게시글 가져오기
		BoardDTO boardDTO = boardDAO.boardDetail(bnum);
		
		//첨부파일 가져오기
		boardDTO.setFiles(
//				upLoadFileDAO.getFiles(
//						String.valueOf(boardDTO.getBnum()), boardDTO.getBcategory()));
				upLoadFileDAO.getFiles(String.valueOf(boardDTO.getBnum())));
		
		//조회수증가
		boardDAO.updateBhit(bnum);
		return boardDTO;
	}
	
	//게시판 전체 레코드수
	@Override
	public long totoalRecordCount() {

		return boardDAO.totoalRecordCount();
	}
	//게시판 카테고리별 레코드 총수 
	@Override
	public long totoalRecordCount(String category) {

		return boardDAO.totoalRecordCount(category);
	}
	//게시판 카테고리별 검색결과 총수 
	@Override
	public long totoalRecordCount(String bcategory, String searchType, String keyword) {

		return boardDAO.totoalRecordCount(bcategory, searchType, keyword);
	}

	
	// 내가 쓴 글
	// 1. 검색어 없을시
	// 내가 쓴 글 전체 카테고리별 레코드 총수
	@Override
	public long myTotalRecordCount(String category, String mid) {
		
		return boardDAO.myTotalRecordCount(category, mid);
	}
	// 내가 쓴 글 카테고리별 게시글 리스트
	@Override
	public List<BoardDTO> myList(String bcategory, String mid, int startRec, int endRec) {
		
		return boardDAO.myList(bcategory, mid, startRec, endRec);
	}

	//2. 검색어 있을시
	// 내가 쓴 글 전체 카테고리별 검색된 레코드 총 수 
	@Override
	public long myTotalRecordCount(String bcategory, String mid, String searchType, String keyword) {
		
		return boardDAO.myTotalRecordCount(bcategory, mid, searchType, keyword);
	}
	//내가 쓴 글 카테고리별 검색결과 목록
	@Override
	public List<BoardDTO> myList(SearchDTO searchDTO, String mid) {
		
		return boardDAO.myList(searchDTO, mid);
	}
	
	

}