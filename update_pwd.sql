USE campus_forum;
UPDATE db_account SET password = '$2a$12$qdxSqyoIu8Ofdge8RatIluLclBWLwGvREsTwZQggFnmd7pkRceyXW' WHERE username = 'admin';
UPDATE db_account SET password = '$2a$12$.8lUGOJHnDtXvqyWFAPWlu7WHp5ZLx3nkwcpAlK26oL6FjgyVyVwq' WHERE username = 'testuser';
SELECT username, password FROM db_account;
