FROM python

ADD . /Arquivos

RUN ls -la

WORKDIR /Arquivos

RUN ls -la

CMD python helloworld.py
