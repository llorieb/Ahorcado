CREATE TABLE palabras (
    id_categoria INTEGER,
    desc_categoria TEXT,
    palabra TEXT
);

INSERT INTO palabras (id_categoria, desc_categoria, palabra) VALUES
(1, 'Paises', 'Argentina'),
(1, 'Paises', 'Estados Unidos'),
(2, 'Ciudades', 'Nueva York'),
(2, 'Ciudades', 'Londres'),
(3, 'Marcas de autos', 'Toyota'),
(3, 'Marcas de autos', 'Ford');
