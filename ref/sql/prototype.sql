ALTER TABLE TB_BOARD DROP PRIMARY KEY CASCADE;
DROP TABLE TB_BOARD CASCADE CONSTRAINTS;
ALTER TABLE TB_USER DROP PRIMARY KEY CASCADE;
DROP TABLE TB_USER CASCADE CONSTRAINTS;
DROP TABLE TB_TEST CASCADE CONSTRAINTS;

-- 사용자
CREATE TABLE TB_USER (
    USER_ID VARCHAR2(100),
    USER_NM VARCHAR2(100),
    USER_PW VARCHAR2(100),
    CONSTRAINT TB_USER_PK PRIMARY KEY (USER_ID)
);

INSERT INTO TB_USER VALUES ('test', '홍길동', '1') ;
INSERT INTO TB_USER VALUES ('test2', '홍길동2', '1') ;

-- 게시판
CREATE TABLE TB_BOARD (
    CODE VARCHAR2(20),
    SEQ NUMBER NOT NULL,
    GRP_SEQ NUMBER NOT NULL,
    REF_SEQ NUMBER DEFAULT 0 NOT NULL,
    SUBJECT VARCHAR2(4000) NOT NULL,
    MEMO CLOB,
    READ_CNT NUMBER(10) DEFAULT 0 NOT NULL,
    USE_YN VARCHAR2(1) DEFAULT 'Y' NOT NULL,
    OPEN_YN VARCHAR2(1) DEFAULT 'Y' NOT NULL,
    CREATE_USER_ID VARCHAR2(100),
    CREATE_DT DATE,
    CREATE_IP VARCHAR2(39),
    UPDATE_USER_ID VARCHAR2(100),
    UPDATE_DT DATE,
    UPDATE_IP VARCHAR2(39),
    CONSTRAINT PK_TB_BOARD PRIMARY KEY (CODE, SEQ),
    CONSTRAINT FK_TB_BOARD_CREATE_USER_ID FOREIGN KEY (CREATE_USER_ID) REFERENCES TB_USER(USER_ID) ON DELETE CASCADE,
    CONSTRAINT FK_TB_BOARD_UPDATE_USER_ID FOREIGN KEY (UPDATE_USER_ID) REFERENCES TB_USER(USER_ID) ON DELETE CASCADE    
);
CREATE INDEX IDX_TB_BOARD_SEQ ON TB_BOARD(SEQ);
CREATE INDEX IDX_TB_BOARD_REF_SEQ ON TB_BOARD(REF_SEQ);
COMMENT ON TABLE TB_BOARD IS '게시판';
COMMENT ON COLUMN TB_BOARD.CODE IS '게시판 코드';
COMMENT ON COLUMN TB_BOARD.SEQ IS '게시판 번호';
COMMENT ON COLUMN TB_BOARD.GRP_SEQ IS '게시판 그룹 번호';
COMMENT ON COLUMN TB_BOARD.REF_SEQ IS '부모 게시판번호';
COMMENT ON COLUMN TB_BOARD.SUBJECT IS '제목';
COMMENT ON COLUMN TB_BOARD.MEMO IS '내용';
COMMENT ON COLUMN TB_BOARD.READ_CNT IS '조회수';
COMMENT ON COLUMN TB_BOARD.USE_YN IS '사용여부';
COMMENT ON COLUMN TB_BOARD.OPEN_YN IS '공개여부';
COMMENT ON COLUMN TB_BOARD.CREATE_USER_ID IS '작성자 아이디';
COMMENT ON COLUMN TB_BOARD.CREATE_DT IS '작성일';
COMMENT ON COLUMN TB_BOARD.CREATE_IP IS '작성자 아이피';
COMMENT ON COLUMN TB_BOARD.UPDATE_USER_ID IS '수정자 아이디';
COMMENT ON COLUMN TB_BOARD.UPDATE_DT IS '수정일';
COMMENT ON COLUMN TB_BOARD.UPDATE_IP IS '작성자 아이피';

-- 테스트
CREATE TABLE TB_TEST (
    DATA1 NUMBER,
    DATA2 VARCHAR2(100)
);
