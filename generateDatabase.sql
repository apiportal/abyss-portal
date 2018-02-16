2018.02.13 Tuesday 19:51:06

192.168.10.40

database: abyssportal
schema: portalschema

user: postgres
pass: postgres

user: abyssuser
pass: User007


-- User: abyssuser
-- DROP USER abyssuser;

CREATE USER abyssuser WITH
  LOGIN
  NOSUPERUSER
  INHERIT
  NOCREATEDB
  NOCREATEROLE
  NOREPLICATION;

----------------------------------

ALTER USER abyssuser
	PASSWORD 'xxxxxx';  
    
----------------------------------

CREATE DATABASE abyssportal
    WITH 
    OWNER = abyssuser
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.UTF-8'
    LC_CTYPE = 'en_US.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

COMMENT ON DATABASE abyssportal
    IS 'Abyss Portal Database';

GRANT ALL ON DATABASE abyssportal TO abyssuser;

GRANT ALL ON DATABASE abyssportal TO postgres;

------------------------------

CREATE SCHEMA portalschema
    AUTHORIZATION abyssuser;

COMMENT ON SCHEMA portalschema
    IS 'Abyss Portal BE Schema';

GRANT ALL ON SCHEMA portalschema TO abyssuser;

GRANT ALL ON SCHEMA portalschema TO postgres;

---------------------------

--ROLLBACK;

CREATE TABLE portalschema.user (
  username VARCHAR(255) NOT NULL,
  password VARCHAR(255) NOT NULL,
  password_salt VARCHAR(255) NOT NULL
);

CREATE TABLE portalschema.user_roles (
  username VARCHAR(255) NOT NULL,
  role VARCHAR(255) NOT NULL
);

CREATE TABLE portalschema.roles_perms (
  role VARCHAR(255) NOT NULL,
  perm VARCHAR(255) NOT NULL
);

ALTER TABLE portalschema.user ADD CONSTRAINT pk_username PRIMARY KEY (username);
ALTER TABLE portalschema.user_roles ADD CONSTRAINT pk_user_roles PRIMARY KEY (username, role);
ALTER TABLE portalschema.roles_perms ADD CONSTRAINT pk_roles_perms PRIMARY KEY (role);

ALTER TABLE portalschema.user_roles ADD CONSTRAINT fk_username FOREIGN KEY (username) REFERENCES portalschema.user(username);
ALTER TABLE portalschema.user_roles ADD CONSTRAINT fk_roles FOREIGN KEY (role) REFERENCES portalschema.roles_perms(role);


GRANT ALL ON TABLE portalschema.roles_perms TO abyssuser;
GRANT ALL ON TABLE portalschema."user" TO abyssuser;
GRANT ALL ON TABLE portalschema.user_roles TO abyssuser;


COMMIT;

--https://jdbc.postgresql.org/documentation/head/connect.html

--You can set the default search_path at the database level:

--  ALTER DATABASE <database_name> SET search_path TO schema1,schema2;
--	ALTER DATABASE abyssportal SET search_path TO portalschema;
    	
--Or at the user or role level:

--	ALTER ROLE <role_name> SET search_path TO schema1,schema2;
--	ALTER ROLE abyssuser SET search_path TO portalschema;

CREATE TABLE portalschema.user_activation (
	--id int4 NOT NULL,
	username VARCHAR(255) NOT NULL,
	expire_date timestamp NOT NULL,
	token varchar(255) NOT NULL,
	deleted bool NOT NULL default false
	--CONSTRAINT user_activation_pkey PRIMARY KEY (id)
);

ALTER TABLE portalschema.user_activation ADD CONSTRAINT fk_user_activation FOREIGN KEY (username) REFERENCES portalschema.user(username);

CREATE UNIQUE INDEX uindex_user_activation_token ON portalschema.user_activation (token DESC);

GRANT ALL ON TABLE portalschema.user_activation TO abyssuser;
    	
COMMIT;