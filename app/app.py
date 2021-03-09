from datetime import datetime
from json import JSONEncoder

from flask import Flask, jsonify, request, Response
from flask_sqlalchemy import SQLAlchemy


class PersonEncoder(JSONEncoder):
    def default(self, o):
        if type(o) is Person:
            json = {
                'id': o.id,
                'first_name': o.first_name,
                'last_name': o.last_name,
            }
            if o.birthdate is not None:
                json['birthdate'] = o.birthdate.isoformat()
            return json
        else:
            return JSONEncoder.default(self, o)


app = Flask(__name__)
app.config['JSON_SORT_KEYS'] = False
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///../demo.sqlite'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.json_encoder = PersonEncoder
db = SQLAlchemy(app)


@app.route('/')
def get_all():
    persons = Person.query.all()
    return jsonify(persons)


@app.route('/<pk>')
def get_one(pk):
    persons = Person.query.get(int(pk))
    return jsonify(persons)


@app.route('/', methods=['POST'])
def post():
    data = request.get_json()
    person = Person(first_name=data['first_name'], last_name=data['last_name'])
    db.session.add(person)
    db.session.commit()
    return Response(status=201, headers={'Location': f'{request.url}{person.id}'})


class Person(db.Model):
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    first_name = db.Column(db.String(80), nullable=False)
    last_name = db.Column(db.String(80), nullable=False)
    birthdate = db.Column(db.Date, nullable=True)

    def __repr__(self):
        return '<Person %r>' % self.id


@app.before_first_request
def init_data():
    db.create_all()
    if not Person.query.all():
        db.session.add(Person(first_name='Joe', last_name='Dalton', birthdate=datetime(1970, 1, 2)))
        db.session.add(Person(first_name='Jack', last_name='Dalton', birthdate=datetime(1973, 4, 5)))
        db.session.add(Person(first_name='William', last_name='Dalton', birthdate=datetime(1976, 7, 8)))
        db.session.add(Person(first_name='Averell', last_name='Dalton', birthdate=datetime(1979, 10, 11)))
        db.session.add(Person(first_name='Ma', last_name='Dalton'))
        db.session.commit()
